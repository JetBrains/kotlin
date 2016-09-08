/**
  ******************************************************************************
  * @file    stm32f4xx_cryp_aes.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides high level functions to encrypt and decrypt an 
  *          input message using AES in ECB/CBC/CTR modes.
  *          It uses the stm32f4xx_cryp.c/.h drivers to access the STM32F4xx CRYP
  *          peripheral.
  *
  *  @verbatim
  *
  *          ===================================================================
  *                                   How to use this driver
  *          ===================================================================
  *          1. Enable The CRYP controller clock using 
  *            RCC_AHB2PeriphClockCmd(RCC_AHB2Periph_CRYP, ENABLE); function.
  *
  *          2. Encrypt and decrypt using AES in ECB Mode using CRYP_AES_ECB()
  *             function.
  *
  *          3. Encrypt and decrypt using AES in CBC Mode using CRYP_AES_CBC()
  *             function.
  *
  *          4. Encrypt and decrypt using AES in CTR Mode using CRYP_AES_CTR()
  *             function.
  *
  *  @endverbatim
  *
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************  
  */

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_cryp.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup CRYP 
  * @brief CRYP driver modules
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
#define AESBUSY_TIMEOUT    ((uint32_t) 0x00010000)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup CRYP_Private_Functions
  * @{
  */ 

/** @defgroup CRYP_Group6 High Level AES functions
 *  @brief   High Level AES functions 
 *
@verbatim   
 ===============================================================================
                          High Level AES functions
 ===============================================================================


@endverbatim
  * @{
  */

/**
  * @brief  Encrypt and decrypt using AES in ECB Mode
  * @param  Mode: encryption or decryption Mode.
  *          This parameter can be one of the following values:
  *            @arg MODE_ENCRYPT: Encryption
  *            @arg MODE_DECRYPT: Decryption
  * @param  Key: Key used for AES algorithm.
  * @param  Keysize: length of the Key, must be a 128, 192 or 256.
  * @param  Input: pointer to the Input buffer.
  * @param  Ilength: length of the Input buffer, must be a multiple of 16.
  * @param  Output: pointer to the returned buffer.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: Operation done
  *          - ERROR: Operation failed
  */
ErrorStatus CRYP_AES_ECB(uint8_t Mode, uint8_t* Key, uint16_t Keysize,
                         uint8_t* Input, uint32_t Ilength, uint8_t* Output)
{
  CRYP_InitTypeDef AES_CRYP_InitStructure;
  CRYP_KeyInitTypeDef AES_CRYP_KeyInitStructure;
  __IO uint32_t counter = 0;
  uint32_t busystatus = 0;
  ErrorStatus status = SUCCESS;
  uint32_t keyaddr    = (uint32_t)Key;
  uint32_t inputaddr  = (uint32_t)Input;
  uint32_t outputaddr = (uint32_t)Output;
  uint32_t i = 0;

  /* Crypto structures initialisation*/
  CRYP_KeyStructInit(&AES_CRYP_KeyInitStructure);

  switch(Keysize)
  {
    case 128:
    AES_CRYP_InitStructure.CRYP_KeySize = CRYP_KeySize_128b;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    case 192:
    AES_CRYP_InitStructure.CRYP_KeySize  = CRYP_KeySize_192b;
    AES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    case 256:
    AES_CRYP_InitStructure.CRYP_KeySize  = CRYP_KeySize_256b;
    AES_CRYP_KeyInitStructure.CRYP_Key0Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key0Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    default:
    break;
  }

  /*------------------ AES Decryption ------------------*/
  if(Mode == MODE_DECRYPT) /* AES decryption */
  {
    /* Flush IN/OUT FIFOs */
    CRYP_FIFOFlush();

    /* Crypto Init for Key preparation for decryption process */
    AES_CRYP_InitStructure.CRYP_AlgoDir = CRYP_AlgoDir_Decrypt;
    AES_CRYP_InitStructure.CRYP_AlgoMode = CRYP_AlgoMode_AES_Key;
    AES_CRYP_InitStructure.CRYP_DataType = CRYP_DataType_32b;
    CRYP_Init(&AES_CRYP_InitStructure);

    /* Key Initialisation */
    CRYP_KeyInit(&AES_CRYP_KeyInitStructure);

    /* Enable Crypto processor */
    CRYP_Cmd(ENABLE);

    /* wait until the Busy flag is RESET */
    do
    {
      busystatus = CRYP_GetFlagStatus(CRYP_FLAG_BUSY);
      counter++;
    }while ((counter != AESBUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
   {
       status = ERROR;
    }
    else
    {
      /* Crypto Init for decryption process */  
      AES_CRYP_InitStructure.CRYP_AlgoDir = CRYP_AlgoDir_Decrypt;
    }
  }
  /*------------------ AES Encryption ------------------*/
  else /* AES encryption */
  {

    CRYP_KeyInit(&AES_CRYP_KeyInitStructure);

    /* Crypto Init for Encryption process */
    AES_CRYP_InitStructure.CRYP_AlgoDir  = CRYP_AlgoDir_Encrypt;
  }

  AES_CRYP_InitStructure.CRYP_AlgoMode = CRYP_AlgoMode_AES_ECB;
  AES_CRYP_InitStructure.CRYP_DataType = CRYP_DataType_8b;
  CRYP_Init(&AES_CRYP_InitStructure);

  /* Flush IN/OUT FIFOs */
  CRYP_FIFOFlush();

  /* Enable Crypto processor */
  CRYP_Cmd(ENABLE);

  for(i=0; ((i<Ilength) && (status != ERROR)); i+=16)
  {

    /* Write the Input block in the IN FIFO */
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;

    /* Wait until the complete message has been processed */
    counter = 0;
    do
    {
      busystatus = CRYP_GetFlagStatus(CRYP_FLAG_BUSY);
      counter++;
    }while ((counter != AESBUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
   {
       status = ERROR;
    }
    else
    {

      /* Read the Output block from the Output FIFO */
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut(); 
      outputaddr+=4;
    }
  }

  /* Disable Crypto */
  CRYP_Cmd(DISABLE);

  return status; 
}

/**
  * @brief  Encrypt and decrypt using AES in CBC Mode
  * @param  Mode: encryption or decryption Mode.
  *          This parameter can be one of the following values:
  *            @arg MODE_ENCRYPT: Encryption
  *            @arg MODE_DECRYPT: Decryption
  * @param  InitVectors: Initialisation Vectors used for AES algorithm.
  * @param  Key: Key used for AES algorithm.
  * @param  Keysize: length of the Key, must be a 128, 192 or 256.
  * @param  Input: pointer to the Input buffer.
  * @param  Ilength: length of the Input buffer, must be a multiple of 16.
  * @param  Output: pointer to the returned buffer.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: Operation done
  *          - ERROR: Operation failed
  */
ErrorStatus CRYP_AES_CBC(uint8_t Mode, uint8_t InitVectors[16], uint8_t *Key,
                         uint16_t Keysize, uint8_t *Input, uint32_t Ilength,
                         uint8_t *Output)
{
  CRYP_InitTypeDef AES_CRYP_InitStructure;
  CRYP_KeyInitTypeDef AES_CRYP_KeyInitStructure;
  CRYP_IVInitTypeDef AES_CRYP_IVInitStructure;
  __IO uint32_t counter = 0;
  uint32_t busystatus = 0;
  ErrorStatus status = SUCCESS;
  uint32_t keyaddr    = (uint32_t)Key;
  uint32_t inputaddr  = (uint32_t)Input;
  uint32_t outputaddr = (uint32_t)Output;
  uint32_t ivaddr     = (uint32_t)InitVectors;
  uint32_t i = 0;

  /* Crypto structures initialisation*/
  CRYP_KeyStructInit(&AES_CRYP_KeyInitStructure);

  switch(Keysize)
  {
    case 128:
    AES_CRYP_InitStructure.CRYP_KeySize = CRYP_KeySize_128b;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    case 192:
    AES_CRYP_InitStructure.CRYP_KeySize  = CRYP_KeySize_192b;
    AES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    case 256:
    AES_CRYP_InitStructure.CRYP_KeySize  = CRYP_KeySize_256b;
    AES_CRYP_KeyInitStructure.CRYP_Key0Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key0Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    default:
    break;
  }

  /* CRYP Initialization Vectors */
  AES_CRYP_IVInitStructure.CRYP_IV0Left = __REV(*(uint32_t*)(ivaddr));
  ivaddr+=4;
  AES_CRYP_IVInitStructure.CRYP_IV0Right= __REV(*(uint32_t*)(ivaddr));
  ivaddr+=4;
  AES_CRYP_IVInitStructure.CRYP_IV1Left = __REV(*(uint32_t*)(ivaddr));
  ivaddr+=4;
  AES_CRYP_IVInitStructure.CRYP_IV1Right= __REV(*(uint32_t*)(ivaddr));


  /*------------------ AES Decryption ------------------*/
  if(Mode == MODE_DECRYPT) /* AES decryption */
  {
    /* Flush IN/OUT FIFOs */
    CRYP_FIFOFlush();

    /* Crypto Init for Key preparation for decryption process */
    AES_CRYP_InitStructure.CRYP_AlgoDir = CRYP_AlgoDir_Decrypt;
    AES_CRYP_InitStructure.CRYP_AlgoMode = CRYP_AlgoMode_AES_Key;
    AES_CRYP_InitStructure.CRYP_DataType = CRYP_DataType_32b;

    CRYP_Init(&AES_CRYP_InitStructure);

    /* Key Initialisation */
    CRYP_KeyInit(&AES_CRYP_KeyInitStructure);

    /* Enable Crypto processor */
    CRYP_Cmd(ENABLE);

    /* wait until the Busy flag is RESET */
    do
    {
      busystatus = CRYP_GetFlagStatus(CRYP_FLAG_BUSY);
      counter++;
    }while ((counter != AESBUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
   {
       status = ERROR;
    }
    else
    {
      /* Crypto Init for decryption process */  
      AES_CRYP_InitStructure.CRYP_AlgoDir = CRYP_AlgoDir_Decrypt;
    }
  }
  /*------------------ AES Encryption ------------------*/
  else /* AES encryption */
  {
    CRYP_KeyInit(&AES_CRYP_KeyInitStructure);

    /* Crypto Init for Encryption process */
    AES_CRYP_InitStructure.CRYP_AlgoDir  = CRYP_AlgoDir_Encrypt;
  }
  AES_CRYP_InitStructure.CRYP_AlgoMode = CRYP_AlgoMode_AES_CBC;
  AES_CRYP_InitStructure.CRYP_DataType = CRYP_DataType_8b;
  CRYP_Init(&AES_CRYP_InitStructure);

  /* CRYP Initialization Vectors */
  CRYP_IVInit(&AES_CRYP_IVInitStructure);

  /* Flush IN/OUT FIFOs */
  CRYP_FIFOFlush();

  /* Enable Crypto processor */
  CRYP_Cmd(ENABLE);


  for(i=0; ((i<Ilength) && (status != ERROR)); i+=16)
  {

    /* Write the Input block in the IN FIFO */
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    /* Wait until the complete message has been processed */
    counter = 0;
    do
    {
      busystatus = CRYP_GetFlagStatus(CRYP_FLAG_BUSY);
      counter++;
    }while ((counter != AESBUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
   {
       status = ERROR;
    }
    else
    {

      /* Read the Output block from the Output FIFO */
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
    }
  }

  /* Disable Crypto */
  CRYP_Cmd(DISABLE);

  return status;
}

/**
  * @brief  Encrypt and decrypt using AES in CTR Mode
  * @param  Mode: encryption or decryption Mode.
  *           This parameter can be one of the following values:
  *            @arg MODE_ENCRYPT: Encryption
  *            @arg MODE_DECRYPT: Decryption
  * @param  InitVectors: Initialisation Vectors used for AES algorithm.
  * @param  Key: Key used for AES algorithm.
  * @param  Keysize: length of the Key, must be a 128, 192 or 256.
  * @param  Input: pointer to the Input buffer.
  * @param  Ilength: length of the Input buffer, must be a multiple of 16.
  * @param  Output: pointer to the returned buffer.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: Operation done
  *          - ERROR: Operation failed
  */
ErrorStatus CRYP_AES_CTR(uint8_t Mode, uint8_t InitVectors[16], uint8_t *Key, 
                         uint16_t Keysize, uint8_t *Input, uint32_t Ilength,
                         uint8_t *Output)
{
  CRYP_InitTypeDef AES_CRYP_InitStructure;
  CRYP_KeyInitTypeDef AES_CRYP_KeyInitStructure;
  CRYP_IVInitTypeDef AES_CRYP_IVInitStructure;
  __IO uint32_t counter = 0;
  uint32_t busystatus = 0;
  ErrorStatus status = SUCCESS;
  uint32_t keyaddr    = (uint32_t)Key;
  uint32_t inputaddr  = (uint32_t)Input;
  uint32_t outputaddr = (uint32_t)Output;
  uint32_t ivaddr     = (uint32_t)InitVectors;
  uint32_t i = 0;

  /* Crypto structures initialisation*/
  CRYP_KeyStructInit(&AES_CRYP_KeyInitStructure);

  switch(Keysize)
  {
    case 128:
    AES_CRYP_InitStructure.CRYP_KeySize = CRYP_KeySize_128b;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    case 192:
    AES_CRYP_InitStructure.CRYP_KeySize  = CRYP_KeySize_192b;
    AES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    case 256:
    AES_CRYP_InitStructure.CRYP_KeySize  = CRYP_KeySize_256b;
    AES_CRYP_KeyInitStructure.CRYP_Key0Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key0Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key2Right= __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Left = __REV(*(uint32_t*)(keyaddr));
    keyaddr+=4;
    AES_CRYP_KeyInitStructure.CRYP_Key3Right= __REV(*(uint32_t*)(keyaddr));
    break;
    default:
    break;
  }
  /* CRYP Initialization Vectors */
  AES_CRYP_IVInitStructure.CRYP_IV0Left = __REV(*(uint32_t*)(ivaddr));
  ivaddr+=4;
  AES_CRYP_IVInitStructure.CRYP_IV0Right= __REV(*(uint32_t*)(ivaddr));
  ivaddr+=4;
  AES_CRYP_IVInitStructure.CRYP_IV1Left = __REV(*(uint32_t*)(ivaddr));
  ivaddr+=4;
  AES_CRYP_IVInitStructure.CRYP_IV1Right= __REV(*(uint32_t*)(ivaddr));

  /* Key Initialisation */
  CRYP_KeyInit(&AES_CRYP_KeyInitStructure);

  /*------------------ AES Decryption ------------------*/
  if(Mode == MODE_DECRYPT) /* AES decryption */
  {
    /* Crypto Init for decryption process */
    AES_CRYP_InitStructure.CRYP_AlgoDir = CRYP_AlgoDir_Decrypt;
  }
  /*------------------ AES Encryption ------------------*/
  else /* AES encryption */
  {
    /* Crypto Init for Encryption process */
    AES_CRYP_InitStructure.CRYP_AlgoDir = CRYP_AlgoDir_Encrypt;
  }
  AES_CRYP_InitStructure.CRYP_AlgoMode = CRYP_AlgoMode_AES_CTR;
  AES_CRYP_InitStructure.CRYP_DataType = CRYP_DataType_8b;
  CRYP_Init(&AES_CRYP_InitStructure);

  /* CRYP Initialization Vectors */
  CRYP_IVInit(&AES_CRYP_IVInitStructure);

  /* Flush IN/OUT FIFOs */
  CRYP_FIFOFlush();

  /* Enable Crypto processor */
  CRYP_Cmd(ENABLE);

  for(i=0; ((i<Ilength) && (status != ERROR)); i+=16)
  {

    /* Write the Input block in the IN FIFO */
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    /* Wait until the complete message has been processed */
    counter = 0;
    do
    {
      busystatus = CRYP_GetFlagStatus(CRYP_FLAG_BUSY);
      counter++;
    }while ((counter != AESBUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
   {
       status = ERROR;
    }
    else
    {

      /* Read the Output block from the Output FIFO */
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
    }
  }
  /* Disable Crypto */
  CRYP_Cmd(DISABLE);

  return status;
}
/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

