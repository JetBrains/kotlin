/**
  ******************************************************************************
  * @file    stm32f4xx_can.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the CAN firmware 
  *          library.
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

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __STM32F4xx_CAN_H
#define __STM32F4xx_CAN_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup CAN
  * @{
  */

/* Exported types ------------------------------------------------------------*/

#define IS_CAN_ALL_PERIPH(PERIPH) (((PERIPH) == CAN1) || \
                                   ((PERIPH) == CAN2))

/** 
  * @brief  CAN init structure definition
  */
typedef struct
{
  uint16_t CAN_Prescaler;   /*!< Specifies the length of a time quantum. 
                                 It ranges from 1 to 1024. */
  
  uint8_t CAN_Mode;         /*!< Specifies the CAN operating mode.
                                 This parameter can be a value of @ref CAN_operating_mode */

  uint8_t CAN_SJW;          /*!< Specifies the maximum number of time quanta 
                                 the CAN hardware is allowed to lengthen or 
                                 shorten a bit to perform resynchronization.
                                 This parameter can be a value of @ref CAN_synchronisation_jump_width */

  uint8_t CAN_BS1;          /*!< Specifies the number of time quanta in Bit 
                                 Segment 1. This parameter can be a value of 
                                 @ref CAN_time_quantum_in_bit_segment_1 */

  uint8_t CAN_BS2;          /*!< Specifies the number of time quanta in Bit Segment 2.
                                 This parameter can be a value of @ref CAN_time_quantum_in_bit_segment_2 */
  
  FunctionalState CAN_TTCM; /*!< Enable or disable the time triggered communication mode.
                                This parameter can be set either to ENABLE or DISABLE. */
  
  FunctionalState CAN_ABOM;  /*!< Enable or disable the automatic bus-off management.
                                  This parameter can be set either to ENABLE or DISABLE. */

  FunctionalState CAN_AWUM;  /*!< Enable or disable the automatic wake-up mode. 
                                  This parameter can be set either to ENABLE or DISABLE. */

  FunctionalState CAN_NART;  /*!< Enable or disable the non-automatic retransmission mode.
                                  This parameter can be set either to ENABLE or DISABLE. */

  FunctionalState CAN_RFLM;  /*!< Enable or disable the Receive FIFO Locked mode.
                                  This parameter can be set either to ENABLE or DISABLE. */

  FunctionalState CAN_TXFP;  /*!< Enable or disable the transmit FIFO priority.
                                  This parameter can be set either to ENABLE or DISABLE. */
} CAN_InitTypeDef;

/** 
  * @brief  CAN filter init structure definition
  */
typedef struct
{
  uint16_t CAN_FilterIdHigh;         /*!< Specifies the filter identification number (MSBs for a 32-bit
                                              configuration, first one for a 16-bit configuration).
                                              This parameter can be a value between 0x0000 and 0xFFFF */

  uint16_t CAN_FilterIdLow;          /*!< Specifies the filter identification number (LSBs for a 32-bit
                                              configuration, second one for a 16-bit configuration).
                                              This parameter can be a value between 0x0000 and 0xFFFF */

  uint16_t CAN_FilterMaskIdHigh;     /*!< Specifies the filter mask number or identification number,
                                              according to the mode (MSBs for a 32-bit configuration,
                                              first one for a 16-bit configuration).
                                              This parameter can be a value between 0x0000 and 0xFFFF */

  uint16_t CAN_FilterMaskIdLow;      /*!< Specifies the filter mask number or identification number,
                                              according to the mode (LSBs for a 32-bit configuration,
                                              second one for a 16-bit configuration).
                                              This parameter can be a value between 0x0000 and 0xFFFF */

  uint16_t CAN_FilterFIFOAssignment; /*!< Specifies the FIFO (0 or 1) which will be assigned to the filter.
                                              This parameter can be a value of @ref CAN_filter_FIFO */
  
  uint8_t CAN_FilterNumber;          /*!< Specifies the filter which will be initialized. It ranges from 0 to 13. */

  uint8_t CAN_FilterMode;            /*!< Specifies the filter mode to be initialized.
                                              This parameter can be a value of @ref CAN_filter_mode */

  uint8_t CAN_FilterScale;           /*!< Specifies the filter scale.
                                              This parameter can be a value of @ref CAN_filter_scale */

  FunctionalState CAN_FilterActivation; /*!< Enable or disable the filter.
                                              This parameter can be set either to ENABLE or DISABLE. */
} CAN_FilterInitTypeDef;

/** 
  * @brief  CAN Tx message structure definition  
  */
typedef struct
{
  uint32_t StdId;  /*!< Specifies the standard identifier.
                        This parameter can be a value between 0 to 0x7FF. */

  uint32_t ExtId;  /*!< Specifies the extended identifier.
                        This parameter can be a value between 0 to 0x1FFFFFFF. */

  uint8_t IDE;     /*!< Specifies the type of identifier for the message that 
                        will be transmitted. This parameter can be a value 
                        of @ref CAN_identifier_type */

  uint8_t RTR;     /*!< Specifies the type of frame for the message that will 
                        be transmitted. This parameter can be a value of 
                        @ref CAN_remote_transmission_request */

  uint8_t DLC;     /*!< Specifies the length of the frame that will be 
                        transmitted. This parameter can be a value between 
                        0 to 8 */

  uint8_t Data[8]; /*!< Contains the data to be transmitted. It ranges from 0 
                        to 0xFF. */
} CanTxMsg;

/** 
  * @brief  CAN Rx message structure definition  
  */
typedef struct
{
  uint32_t StdId;  /*!< Specifies the standard identifier.
                        This parameter can be a value between 0 to 0x7FF. */

  uint32_t ExtId;  /*!< Specifies the extended identifier.
                        This parameter can be a value between 0 to 0x1FFFFFFF. */

  uint8_t IDE;     /*!< Specifies the type of identifier for the message that 
                        will be received. This parameter can be a value of 
                        @ref CAN_identifier_type */

  uint8_t RTR;     /*!< Specifies the type of frame for the received message.
                        This parameter can be a value of 
                        @ref CAN_remote_transmission_request */

  uint8_t DLC;     /*!< Specifies the length of the frame that will be received.
                        This parameter can be a value between 0 to 8 */

  uint8_t Data[8]; /*!< Contains the data to be received. It ranges from 0 to 
                        0xFF. */

  uint8_t FMI;     /*!< Specifies the index of the filter the message stored in 
                        the mailbox passes through. This parameter can be a 
                        value between 0 to 0xFF */
} CanRxMsg;

/* Exported constants --------------------------------------------------------*/

/** @defgroup CAN_Exported_Constants
  * @{
  */

/** @defgroup CAN_InitStatus 
  * @{
  */

#define CAN_InitStatus_Failed              ((uint8_t)0x00) /*!< CAN initialization failed */
#define CAN_InitStatus_Success             ((uint8_t)0x01) /*!< CAN initialization OK */


/* Legacy defines */
#define CANINITFAILED    CAN_InitStatus_Failed
#define CANINITOK        CAN_InitStatus_Success
/**
  * @}
  */

/** @defgroup CAN_operating_mode 
  * @{
  */

#define CAN_Mode_Normal             ((uint8_t)0x00)  /*!< normal mode */
#define CAN_Mode_LoopBack           ((uint8_t)0x01)  /*!< loopback mode */
#define CAN_Mode_Silent             ((uint8_t)0x02)  /*!< silent mode */
#define CAN_Mode_Silent_LoopBack    ((uint8_t)0x03)  /*!< loopback combined with silent mode */

#define IS_CAN_MODE(MODE) (((MODE) == CAN_Mode_Normal) || \
                           ((MODE) == CAN_Mode_LoopBack)|| \
                           ((MODE) == CAN_Mode_Silent) || \
                           ((MODE) == CAN_Mode_Silent_LoopBack))
/**
  * @}
  */


 /**
  * @defgroup CAN_operating_mode 
  * @{
  */  
#define CAN_OperatingMode_Initialization  ((uint8_t)0x00) /*!< Initialization mode */
#define CAN_OperatingMode_Normal          ((uint8_t)0x01) /*!< Normal mode */
#define CAN_OperatingMode_Sleep           ((uint8_t)0x02) /*!< sleep mode */


#define IS_CAN_OPERATING_MODE(MODE) (((MODE) == CAN_OperatingMode_Initialization) ||\
                                    ((MODE) == CAN_OperatingMode_Normal)|| \
																		((MODE) == CAN_OperatingMode_Sleep))
/**
  * @}
  */
  
/**
  * @defgroup CAN_operating_mode_status
  * @{
  */  

#define CAN_ModeStatus_Failed    ((uint8_t)0x00)                /*!< CAN entering the specific mode failed */
#define CAN_ModeStatus_Success   ((uint8_t)!CAN_ModeStatus_Failed)   /*!< CAN entering the specific mode Succeed */
/**
  * @}
  */

/** @defgroup CAN_synchronisation_jump_width 
  * @{
  */
#define CAN_SJW_1tq                 ((uint8_t)0x00)  /*!< 1 time quantum */
#define CAN_SJW_2tq                 ((uint8_t)0x01)  /*!< 2 time quantum */
#define CAN_SJW_3tq                 ((uint8_t)0x02)  /*!< 3 time quantum */
#define CAN_SJW_4tq                 ((uint8_t)0x03)  /*!< 4 time quantum */

#define IS_CAN_SJW(SJW) (((SJW) == CAN_SJW_1tq) || ((SJW) == CAN_SJW_2tq)|| \
                         ((SJW) == CAN_SJW_3tq) || ((SJW) == CAN_SJW_4tq))
/**
  * @}
  */

/** @defgroup CAN_time_quantum_in_bit_segment_1 
  * @{
  */
#define CAN_BS1_1tq                 ((uint8_t)0x00)  /*!< 1 time quantum */
#define CAN_BS1_2tq                 ((uint8_t)0x01)  /*!< 2 time quantum */
#define CAN_BS1_3tq                 ((uint8_t)0x02)  /*!< 3 time quantum */
#define CAN_BS1_4tq                 ((uint8_t)0x03)  /*!< 4 time quantum */
#define CAN_BS1_5tq                 ((uint8_t)0x04)  /*!< 5 time quantum */
#define CAN_BS1_6tq                 ((uint8_t)0x05)  /*!< 6 time quantum */
#define CAN_BS1_7tq                 ((uint8_t)0x06)  /*!< 7 time quantum */
#define CAN_BS1_8tq                 ((uint8_t)0x07)  /*!< 8 time quantum */
#define CAN_BS1_9tq                 ((uint8_t)0x08)  /*!< 9 time quantum */
#define CAN_BS1_10tq                ((uint8_t)0x09)  /*!< 10 time quantum */
#define CAN_BS1_11tq                ((uint8_t)0x0A)  /*!< 11 time quantum */
#define CAN_BS1_12tq                ((uint8_t)0x0B)  /*!< 12 time quantum */
#define CAN_BS1_13tq                ((uint8_t)0x0C)  /*!< 13 time quantum */
#define CAN_BS1_14tq                ((uint8_t)0x0D)  /*!< 14 time quantum */
#define CAN_BS1_15tq                ((uint8_t)0x0E)  /*!< 15 time quantum */
#define CAN_BS1_16tq                ((uint8_t)0x0F)  /*!< 16 time quantum */

#define IS_CAN_BS1(BS1) ((BS1) <= CAN_BS1_16tq)
/**
  * @}
  */

/** @defgroup CAN_time_quantum_in_bit_segment_2 
  * @{
  */
#define CAN_BS2_1tq                 ((uint8_t)0x00)  /*!< 1 time quantum */
#define CAN_BS2_2tq                 ((uint8_t)0x01)  /*!< 2 time quantum */
#define CAN_BS2_3tq                 ((uint8_t)0x02)  /*!< 3 time quantum */
#define CAN_BS2_4tq                 ((uint8_t)0x03)  /*!< 4 time quantum */
#define CAN_BS2_5tq                 ((uint8_t)0x04)  /*!< 5 time quantum */
#define CAN_BS2_6tq                 ((uint8_t)0x05)  /*!< 6 time quantum */
#define CAN_BS2_7tq                 ((uint8_t)0x06)  /*!< 7 time quantum */
#define CAN_BS2_8tq                 ((uint8_t)0x07)  /*!< 8 time quantum */

#define IS_CAN_BS2(BS2) ((BS2) <= CAN_BS2_8tq)
/**
  * @}
  */

/** @defgroup CAN_clock_prescaler 
  * @{
  */
#define IS_CAN_PRESCALER(PRESCALER) (((PRESCALER) >= 1) && ((PRESCALER) <= 1024))
/**
  * @}
  */

/** @defgroup CAN_filter_number 
  * @{
  */
#define IS_CAN_FILTER_NUMBER(NUMBER) ((NUMBER) <= 27)
/**
  * @}
  */

/** @defgroup CAN_filter_mode 
  * @{
  */
#define CAN_FilterMode_IdMask       ((uint8_t)0x00)  /*!< identifier/mask mode */
#define CAN_FilterMode_IdList       ((uint8_t)0x01)  /*!< identifier list mode */

#define IS_CAN_FILTER_MODE(MODE) (((MODE) == CAN_FilterMode_IdMask) || \
                                  ((MODE) == CAN_FilterMode_IdList))
/**
  * @}
  */

/** @defgroup CAN_filter_scale 
  * @{
  */
#define CAN_FilterScale_16bit       ((uint8_t)0x00) /*!< Two 16-bit filters */
#define CAN_FilterScale_32bit       ((uint8_t)0x01) /*!< One 32-bit filter */

#define IS_CAN_FILTER_SCALE(SCALE) (((SCALE) == CAN_FilterScale_16bit) || \
                                    ((SCALE) == CAN_FilterScale_32bit))
/**
  * @}
  */

/** @defgroup CAN_filter_FIFO
  * @{
  */
#define CAN_Filter_FIFO0             ((uint8_t)0x00)  /*!< Filter FIFO 0 assignment for filter x */
#define CAN_Filter_FIFO1             ((uint8_t)0x01)  /*!< Filter FIFO 1 assignment for filter x */
#define IS_CAN_FILTER_FIFO(FIFO) (((FIFO) == CAN_FilterFIFO0) || \
                                  ((FIFO) == CAN_FilterFIFO1))

/* Legacy defines */
#define CAN_FilterFIFO0  CAN_Filter_FIFO0
#define CAN_FilterFIFO1  CAN_Filter_FIFO1
/**
  * @}
  */

/** @defgroup CAN_Start_bank_filter_for_slave_CAN 
  * @{
  */
#define IS_CAN_BANKNUMBER(BANKNUMBER) (((BANKNUMBER) >= 1) && ((BANKNUMBER) <= 27))
/**
  * @}
  */

/** @defgroup CAN_Tx 
  * @{
  */
#define IS_CAN_TRANSMITMAILBOX(TRANSMITMAILBOX) ((TRANSMITMAILBOX) <= ((uint8_t)0x02))
#define IS_CAN_STDID(STDID)   ((STDID) <= ((uint32_t)0x7FF))
#define IS_CAN_EXTID(EXTID)   ((EXTID) <= ((uint32_t)0x1FFFFFFF))
#define IS_CAN_DLC(DLC)       ((DLC) <= ((uint8_t)0x08))
/**
  * @}
  */

/** @defgroup CAN_identifier_type 
  * @{
  */
#define CAN_Id_Standard             ((uint32_t)0x00000000)  /*!< Standard Id */
#define CAN_Id_Extended             ((uint32_t)0x00000004)  /*!< Extended Id */
#define IS_CAN_IDTYPE(IDTYPE) (((IDTYPE) == CAN_Id_Standard) || \
                               ((IDTYPE) == CAN_Id_Extended))

/* Legacy defines */
#define CAN_ID_STD      CAN_Id_Standard           
#define CAN_ID_EXT      CAN_Id_Extended
/**
  * @}
  */

/** @defgroup CAN_remote_transmission_request 
  * @{
  */
#define CAN_RTR_Data                ((uint32_t)0x00000000)  /*!< Data frame */
#define CAN_RTR_Remote              ((uint32_t)0x00000002)  /*!< Remote frame */
#define IS_CAN_RTR(RTR) (((RTR) == CAN_RTR_Data) || ((RTR) == CAN_RTR_Remote))

/* Legacy defines */
#define CAN_RTR_DATA     CAN_RTR_Data         
#define CAN_RTR_REMOTE   CAN_RTR_Remote
/**
  * @}
  */

/** @defgroup CAN_transmit_constants 
  * @{
  */
#define CAN_TxStatus_Failed         ((uint8_t)0x00)/*!< CAN transmission failed */
#define CAN_TxStatus_Ok             ((uint8_t)0x01) /*!< CAN transmission succeeded */
#define CAN_TxStatus_Pending        ((uint8_t)0x02) /*!< CAN transmission pending */
#define CAN_TxStatus_NoMailBox      ((uint8_t)0x04) /*!< CAN cell did not provide 
                                                         an empty mailbox */
/* Legacy defines */	
#define CANTXFAILED                  CAN_TxStatus_Failed
#define CANTXOK                      CAN_TxStatus_Ok
#define CANTXPENDING                 CAN_TxStatus_Pending
#define CAN_NO_MB                    CAN_TxStatus_NoMailBox
/**
  * @}
  */

/** @defgroup CAN_receive_FIFO_number_constants 
  * @{
  */
#define CAN_FIFO0                 ((uint8_t)0x00) /*!< CAN FIFO 0 used to receive */
#define CAN_FIFO1                 ((uint8_t)0x01) /*!< CAN FIFO 1 used to receive */

#define IS_CAN_FIFO(FIFO) (((FIFO) == CAN_FIFO0) || ((FIFO) == CAN_FIFO1))
/**
  * @}
  */

/** @defgroup CAN_sleep_constants 
  * @{
  */
#define CAN_Sleep_Failed     ((uint8_t)0x00) /*!< CAN did not enter the sleep mode */
#define CAN_Sleep_Ok         ((uint8_t)0x01) /*!< CAN entered the sleep mode */

/* Legacy defines */	
#define CANSLEEPFAILED   CAN_Sleep_Failed
#define CANSLEEPOK       CAN_Sleep_Ok
/**
  * @}
  */

/** @defgroup CAN_wake_up_constants 
  * @{
  */
#define CAN_WakeUp_Failed        ((uint8_t)0x00) /*!< CAN did not leave the sleep mode */
#define CAN_WakeUp_Ok            ((uint8_t)0x01) /*!< CAN leaved the sleep mode */

/* Legacy defines */
#define CANWAKEUPFAILED   CAN_WakeUp_Failed        
#define CANWAKEUPOK       CAN_WakeUp_Ok        
/**
  * @}
  */

/**
  * @defgroup CAN_Error_Code_constants
  * @{
  */                                                         
#define CAN_ErrorCode_NoErr           ((uint8_t)0x00) /*!< No Error */ 
#define	CAN_ErrorCode_StuffErr        ((uint8_t)0x10) /*!< Stuff Error */ 
#define	CAN_ErrorCode_FormErr         ((uint8_t)0x20) /*!< Form Error */ 
#define	CAN_ErrorCode_ACKErr          ((uint8_t)0x30) /*!< Acknowledgment Error */ 
#define	CAN_ErrorCode_BitRecessiveErr ((uint8_t)0x40) /*!< Bit Recessive Error */ 
#define	CAN_ErrorCode_BitDominantErr  ((uint8_t)0x50) /*!< Bit Dominant Error */ 
#define	CAN_ErrorCode_CRCErr          ((uint8_t)0x60) /*!< CRC Error  */ 
#define	CAN_ErrorCode_SoftwareSetErr  ((uint8_t)0x70) /*!< Software Set Error */ 
/**
  * @}
  */

/** @defgroup CAN_flags 
  * @{
  */
/* If the flag is 0x3XXXXXXX, it means that it can be used with CAN_GetFlagStatus()
   and CAN_ClearFlag() functions. */
/* If the flag is 0x1XXXXXXX, it means that it can only be used with 
   CAN_GetFlagStatus() function.  */

/* Transmit Flags */
#define CAN_FLAG_RQCP0             ((uint32_t)0x38000001) /*!< Request MailBox0 Flag */
#define CAN_FLAG_RQCP1             ((uint32_t)0x38000100) /*!< Request MailBox1 Flag */
#define CAN_FLAG_RQCP2             ((uint32_t)0x38010000) /*!< Request MailBox2 Flag */

/* Receive Flags */
#define CAN_FLAG_FMP0              ((uint32_t)0x12000003) /*!< FIFO 0 Message Pending Flag */
#define CAN_FLAG_FF0               ((uint32_t)0x32000008) /*!< FIFO 0 Full Flag            */
#define CAN_FLAG_FOV0              ((uint32_t)0x32000010) /*!< FIFO 0 Overrun Flag         */
#define CAN_FLAG_FMP1              ((uint32_t)0x14000003) /*!< FIFO 1 Message Pending Flag */
#define CAN_FLAG_FF1               ((uint32_t)0x34000008) /*!< FIFO 1 Full Flag            */
#define CAN_FLAG_FOV1              ((uint32_t)0x34000010) /*!< FIFO 1 Overrun Flag         */

/* Operating Mode Flags */
#define CAN_FLAG_WKU               ((uint32_t)0x31000008) /*!< Wake up Flag */
#define CAN_FLAG_SLAK              ((uint32_t)0x31000012) /*!< Sleep acknowledge Flag */
/* @note When SLAK interrupt is disabled (SLKIE=0), no polling on SLAKI is possible. 
         In this case the SLAK bit can be polled.*/

/* Error Flags */
#define CAN_FLAG_EWG               ((uint32_t)0x10F00001) /*!< Error Warning Flag   */
#define CAN_FLAG_EPV               ((uint32_t)0x10F00002) /*!< Error Passive Flag   */
#define CAN_FLAG_BOF               ((uint32_t)0x10F00004) /*!< Bus-Off Flag         */
#define CAN_FLAG_LEC               ((uint32_t)0x30F00070) /*!< Last error code Flag */

#define IS_CAN_GET_FLAG(FLAG) (((FLAG) == CAN_FLAG_LEC)  || ((FLAG) == CAN_FLAG_BOF)   || \
                               ((FLAG) == CAN_FLAG_EPV)  || ((FLAG) == CAN_FLAG_EWG)   || \
                               ((FLAG) == CAN_FLAG_WKU)  || ((FLAG) == CAN_FLAG_FOV0)  || \
                               ((FLAG) == CAN_FLAG_FF0)  || ((FLAG) == CAN_FLAG_FMP0)  || \
                               ((FLAG) == CAN_FLAG_FOV1) || ((FLAG) == CAN_FLAG_FF1)   || \
                               ((FLAG) == CAN_FLAG_FMP1) || ((FLAG) == CAN_FLAG_RQCP2) || \
                               ((FLAG) == CAN_FLAG_RQCP1)|| ((FLAG) == CAN_FLAG_RQCP0) || \
                               ((FLAG) == CAN_FLAG_SLAK ))

#define IS_CAN_CLEAR_FLAG(FLAG)(((FLAG) == CAN_FLAG_LEC) || ((FLAG) == CAN_FLAG_RQCP2) || \
                                ((FLAG) == CAN_FLAG_RQCP1)  || ((FLAG) == CAN_FLAG_RQCP0) || \
                                ((FLAG) == CAN_FLAG_FF0)  || ((FLAG) == CAN_FLAG_FOV0) ||\
                                ((FLAG) == CAN_FLAG_FF1) || ((FLAG) == CAN_FLAG_FOV1) || \
                                ((FLAG) == CAN_FLAG_WKU) || ((FLAG) == CAN_FLAG_SLAK))
/**
  * @}
  */

  
/** @defgroup CAN_interrupts 
  * @{
  */ 
#define CAN_IT_TME                  ((uint32_t)0x00000001) /*!< Transmit mailbox empty Interrupt*/

/* Receive Interrupts */
#define CAN_IT_FMP0                 ((uint32_t)0x00000002) /*!< FIFO 0 message pending Interrupt*/
#define CAN_IT_FF0                  ((uint32_t)0x00000004) /*!< FIFO 0 full Interrupt*/
#define CAN_IT_FOV0                 ((uint32_t)0x00000008) /*!< FIFO 0 overrun Interrupt*/
#define CAN_IT_FMP1                 ((uint32_t)0x00000010) /*!< FIFO 1 message pending Interrupt*/
#define CAN_IT_FF1                  ((uint32_t)0x00000020) /*!< FIFO 1 full Interrupt*/
#define CAN_IT_FOV1                 ((uint32_t)0x00000040) /*!< FIFO 1 overrun Interrupt*/

/* Operating Mode Interrupts */
#define CAN_IT_WKU                  ((uint32_t)0x00010000) /*!< Wake-up Interrupt*/
#define CAN_IT_SLK                  ((uint32_t)0x00020000) /*!< Sleep acknowledge Interrupt*/

/* Error Interrupts */
#define CAN_IT_EWG                  ((uint32_t)0x00000100) /*!< Error warning Interrupt*/
#define CAN_IT_EPV                  ((uint32_t)0x00000200) /*!< Error passive Interrupt*/
#define CAN_IT_BOF                  ((uint32_t)0x00000400) /*!< Bus-off Interrupt*/
#define CAN_IT_LEC                  ((uint32_t)0x00000800) /*!< Last error code Interrupt*/
#define CAN_IT_ERR                  ((uint32_t)0x00008000) /*!< Error Interrupt*/

/* Flags named as Interrupts : kept only for FW compatibility */
#define CAN_IT_RQCP0   CAN_IT_TME
#define CAN_IT_RQCP1   CAN_IT_TME
#define CAN_IT_RQCP2   CAN_IT_TME


#define IS_CAN_IT(IT)        (((IT) == CAN_IT_TME) || ((IT) == CAN_IT_FMP0)  ||\
                             ((IT) == CAN_IT_FF0)  || ((IT) == CAN_IT_FOV0)  ||\
                             ((IT) == CAN_IT_FMP1) || ((IT) == CAN_IT_FF1)   ||\
                             ((IT) == CAN_IT_FOV1) || ((IT) == CAN_IT_EWG)   ||\
                             ((IT) == CAN_IT_EPV)  || ((IT) == CAN_IT_BOF)   ||\
                             ((IT) == CAN_IT_LEC)  || ((IT) == CAN_IT_ERR)   ||\
                             ((IT) == CAN_IT_WKU)  || ((IT) == CAN_IT_SLK))

#define IS_CAN_CLEAR_IT(IT) (((IT) == CAN_IT_TME) || ((IT) == CAN_IT_FF0)    ||\
                             ((IT) == CAN_IT_FOV0)|| ((IT) == CAN_IT_FF1)    ||\
                             ((IT) == CAN_IT_FOV1)|| ((IT) == CAN_IT_EWG)    ||\
                             ((IT) == CAN_IT_EPV) || ((IT) == CAN_IT_BOF)    ||\
                             ((IT) == CAN_IT_LEC) || ((IT) == CAN_IT_ERR)    ||\
                             ((IT) == CAN_IT_WKU) || ((IT) == CAN_IT_SLK))
/**
  * @}
  */

/**
  * @}
  */

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/  

/*  Function used to set the CAN configuration to the default reset state *****/ 
void CAN_DeInit(CAN_TypeDef* CANx);

/* Initialization and Configuration functions *********************************/ 
uint8_t CAN_Init(CAN_TypeDef* CANx, CAN_InitTypeDef* CAN_InitStruct);
void CAN_FilterInit(CAN_FilterInitTypeDef* CAN_FilterInitStruct);
void CAN_StructInit(CAN_InitTypeDef* CAN_InitStruct);
void CAN_SlaveStartBank(uint8_t CAN_BankNumber); 
void CAN_DBGFreeze(CAN_TypeDef* CANx, FunctionalState NewState);
void CAN_TTComModeCmd(CAN_TypeDef* CANx, FunctionalState NewState);

/* CAN Frames Transmission functions ******************************************/
uint8_t CAN_Transmit(CAN_TypeDef* CANx, CanTxMsg* TxMessage);
uint8_t CAN_TransmitStatus(CAN_TypeDef* CANx, uint8_t TransmitMailbox);
void CAN_CancelTransmit(CAN_TypeDef* CANx, uint8_t Mailbox);

/* CAN Frames Reception functions *********************************************/
void CAN_Receive(CAN_TypeDef* CANx, uint8_t FIFONumber, CanRxMsg* RxMessage);
void CAN_FIFORelease(CAN_TypeDef* CANx, uint8_t FIFONumber);
uint8_t CAN_MessagePending(CAN_TypeDef* CANx, uint8_t FIFONumber);

/* Operation modes functions **************************************************/
uint8_t CAN_OperatingModeRequest(CAN_TypeDef* CANx, uint8_t CAN_OperatingMode);
uint8_t CAN_Sleep(CAN_TypeDef* CANx);
uint8_t CAN_WakeUp(CAN_TypeDef* CANx);

/* CAN Bus Error management functions *****************************************/
uint8_t CAN_GetLastErrorCode(CAN_TypeDef* CANx);
uint8_t CAN_GetReceiveErrorCounter(CAN_TypeDef* CANx);
uint8_t CAN_GetLSBTransmitErrorCounter(CAN_TypeDef* CANx);

/* Interrupts and flags management functions **********************************/
void CAN_ITConfig(CAN_TypeDef* CANx, uint32_t CAN_IT, FunctionalState NewState);
FlagStatus CAN_GetFlagStatus(CAN_TypeDef* CANx, uint32_t CAN_FLAG);
void CAN_ClearFlag(CAN_TypeDef* CANx, uint32_t CAN_FLAG);
ITStatus CAN_GetITStatus(CAN_TypeDef* CANx, uint32_t CAN_IT);
void CAN_ClearITPendingBit(CAN_TypeDef* CANx, uint32_t CAN_IT);

#ifdef __cplusplus
}
#endif

#endif /* __STM32F4xx_CAN_H */

/**
  * @}
  */

/**
  * @}
  */

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
