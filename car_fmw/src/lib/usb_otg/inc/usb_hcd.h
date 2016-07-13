/**
  ******************************************************************************
  * @file    usb_hcd.h
  * @author  MCD Application Team
  * @version V2.0.0
  * @date    22-July-2011
  * @brief   Host layer Header file
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
#ifndef __USB_HCD_H__
#define __USB_HCD_H__

/* Includes ------------------------------------------------------------------*/
#include "usb_regs.h"
#include "usb_core.h"


/** @addtogroup USB_OTG_DRIVER
  * @{
  */
  
/** @defgroup USB_HCD
  * @brief This file is the 
  * @{
  */ 


/** @defgroup USB_HCD_Exported_Defines
  * @{
  */ 
/**
  * @}
  */ 


/** @defgroup USB_HCD_Exported_Types
  * @{
  */ 
/**
  * @}
  */ 


/** @defgroup USB_HCD_Exported_Macros
  * @{
  */ 
/**
  * @}
  */ 

/** @defgroup USB_HCD_Exported_Variables
  * @{
  */ 
/**
  * @}
  */ 

/** @defgroup USB_HCD_Exported_FunctionsPrototype
  * @{
  */ 
uint32_t  HCD_Init                 (USB_OTG_CORE_HANDLE *pdev ,
                                    USB_OTG_CORE_ID_TypeDef coreID);
uint32_t  HCD_HC_Init              (USB_OTG_CORE_HANDLE *pdev , 
                                    uint8_t hc_num); 
uint32_t  HCD_SubmitRequest        (USB_OTG_CORE_HANDLE *pdev , 
                                    uint8_t hc_num) ;
uint32_t  HCD_GetCurrentSpeed      (USB_OTG_CORE_HANDLE *pdev);
uint32_t  HCD_ResetPort            (USB_OTG_CORE_HANDLE *pdev);
uint32_t  HCD_IsDeviceConnected    (USB_OTG_CORE_HANDLE *pdev);
uint32_t  HCD_GetCurrentFrame      (USB_OTG_CORE_HANDLE *pdev) ;
URB_STATE HCD_GetURB_State         (USB_OTG_CORE_HANDLE *pdev,  uint8_t ch_num); 
uint32_t  HCD_GetXferCnt           (USB_OTG_CORE_HANDLE *pdev,  uint8_t ch_num); 
HC_STATUS HCD_GetHCState           (USB_OTG_CORE_HANDLE *pdev,  uint8_t ch_num) ;
/**
  * @}
  */ 

#endif //__USB_HCD_H__


/**
  * @}
  */ 

/**
  * @}
  */ 
/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

